/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.core.ModuleFields;

@Layout
public interface ClassLayout extends ModuleLayout {

    DynamicObjectFactory createClassShape(DynamicObject logicalClass,
                                          DynamicObject metaClass);

    DynamicObject createClass(DynamicObjectFactory factory,
                              ModuleFields model,
                              boolean isSingleton,
                              @Nullable DynamicObject attached,
                              @Nullable DynamicObjectFactory instanceFactory);

    boolean isClass(DynamicObject object);
    boolean isClass(Object object);

    boolean getIsSingleton(DynamicObject object);

    DynamicObject getAttached(DynamicObject object);

    DynamicObjectFactory getInstanceFactory(DynamicObject object);
    void setInstanceFactoryUnsafe(DynamicObject object, DynamicObjectFactory instanceFactory);

}
