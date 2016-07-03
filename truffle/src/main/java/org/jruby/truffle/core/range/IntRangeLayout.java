/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.range;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface IntRangeLayout extends BasicObjectLayout {

    DynamicObjectFactory createIntRangeShape(DynamicObject logicalClass, DynamicObject metaClass);

    DynamicObject createIntRange(
            DynamicObjectFactory factory,
            boolean excludedEnd,
            int begin,
            int end);

    boolean isIntRange(Object object);
    boolean isIntRange(DynamicObject object);

    boolean getExcludedEnd(DynamicObject object);

    int getBegin(DynamicObject object);

    int getEnd(DynamicObject object);

}
