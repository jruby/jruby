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

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;

@Layout
public interface BindingLayout extends BasicObjectLayout {

    DynamicObjectFactory createBindingShape(DynamicObject logicalClass, DynamicObject metaClass);

    DynamicObject createBinding(DynamicObjectFactory factory, @Nullable Object self, @Nullable MaterializedFrame frame);

    boolean isBinding(DynamicObject object);

    Object getSelf(DynamicObject object);

    void setSelf(DynamicObject object, Object self);

    MaterializedFrame getFrame(DynamicObject object);

    void setFrame(DynamicObject object, MaterializedFrame frame);

}
