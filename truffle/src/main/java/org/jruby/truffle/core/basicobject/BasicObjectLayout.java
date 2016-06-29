/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.basicobject;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import org.jruby.truffle.language.RubyObjectType;

@Layout(objectTypeSuperclass = RubyObjectType.class, implicitCastIntToLong = true)
public interface BasicObjectLayout {

    DynamicObjectFactory createBasicObjectShape(@Nullable DynamicObject logicalClass,
                                                @Nullable DynamicObject metaClass);

    DynamicObject createBasicObject(DynamicObjectFactory factory);

    boolean isBasicObject(Object object);

    DynamicObjectFactory setLogicalClass(DynamicObjectFactory factory, DynamicObject value);
    DynamicObject getLogicalClass(ObjectType objectType);
    DynamicObject getLogicalClass(DynamicObject object);
    void setLogicalClass(DynamicObject object, DynamicObject value);

    DynamicObjectFactory setMetaClass(DynamicObjectFactory factory, DynamicObject value);
    DynamicObject getMetaClass(ObjectType objectType);
    DynamicObject getMetaClass(DynamicObject object);
    void setMetaClass(DynamicObject object, DynamicObject value);
}
