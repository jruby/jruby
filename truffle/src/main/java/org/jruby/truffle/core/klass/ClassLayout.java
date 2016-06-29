/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.klass;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import org.jruby.truffle.core.module.ModuleFields;
import org.jruby.truffle.core.module.ModuleLayout;

@Layout
public interface ClassLayout extends ModuleLayout {

    DynamicObjectFactory createClassShape(DynamicObject logicalClass,
                                          DynamicObject metaClass);

    DynamicObject createClass(DynamicObjectFactory factory,
                              ModuleFields fields,
                              boolean isSingleton,
                              @Nullable DynamicObject attached,
                              @Nullable DynamicObjectFactory instanceFactory,
                              @Nullable DynamicObject superclass);

    boolean isClass(DynamicObject object);
    boolean isClass(Object object);

    boolean getIsSingleton(DynamicObject object);

    DynamicObject getAttached(DynamicObject object);

    DynamicObjectFactory getInstanceFactory(DynamicObject object);
    void setInstanceFactoryUnsafe(DynamicObject object, DynamicObjectFactory value);

    DynamicObject getSuperclass(DynamicObject object);
    void setSuperclass(DynamicObject object, DynamicObject value);

}
