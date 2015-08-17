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
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.om.dsl.api.Layout;

@Layout
public interface FiberLayout extends BasicObjectLayout {

    DynamicObjectFactory createFiberShape(DynamicObject logicalClass, DynamicObject metaClass);

    DynamicObject createFiber(DynamicObjectFactory factory, FiberNodes.FiberFields fields);

    boolean isFiber(DynamicObject object);

    FiberNodes.FiberFields getFields(DynamicObject object);

}
