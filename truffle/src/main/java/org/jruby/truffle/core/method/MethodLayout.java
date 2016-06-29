/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.method;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import org.jruby.truffle.language.methods.InternalMethod;

@Layout
public interface MethodLayout extends BasicObjectLayout {

    DynamicObjectFactory createMethodShape(DynamicObject logicalClass,
                                           DynamicObject metaClass);

    DynamicObject createMethod(DynamicObjectFactory factory,
                               Object receiver,
                               InternalMethod method);

    boolean isMethod(DynamicObject object);
    boolean isMethod(Object object);
    boolean isMethod(ObjectType objectType);

    Object getReceiver(DynamicObject object);

    InternalMethod getMethod(DynamicObject object);

}
