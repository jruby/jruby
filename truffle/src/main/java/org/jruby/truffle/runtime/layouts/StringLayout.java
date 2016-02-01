/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.layouts;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.rope.Rope;

@Layout
public interface StringLayout extends BasicObjectLayout {

    DynamicObjectFactory createStringShape(DynamicObject logicalClass,
                                           DynamicObject metaClass);

    DynamicObject createString(DynamicObjectFactory factory,
                               Rope rope,
                               @Nullable DynamicObject rubiniusDataArray);

    boolean isString(ObjectType objectType);
    boolean isString(DynamicObject dynamicObject);
    boolean isString(Object dynamicObject);

    Rope getRope(DynamicObject object);
    void setRope(DynamicObject object, Rope rope);

    DynamicObject getRubiniusDataArray(DynamicObject object);
    void setRubiniusDataArray(DynamicObject object, DynamicObject rubiniusDataArray);

}
