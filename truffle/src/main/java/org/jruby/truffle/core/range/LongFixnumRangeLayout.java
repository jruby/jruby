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
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import com.oracle.truffle.api.object.dsl.Layout;

@Layout
public interface LongFixnumRangeLayout extends BasicObjectLayout {

    DynamicObjectFactory createLongFixnumRangeShape(DynamicObject logicalClass,
                                                    DynamicObject metaClass);

    DynamicObject createLongFixnumRange(DynamicObjectFactory factory,
                                        boolean excludedEnd,
                                        long begin,
                                        long end);

    boolean isLongFixnumRange(Object object);
    boolean isLongFixnumRange(DynamicObject object);

    boolean getExcludedEnd(DynamicObject object);

    long getBegin(DynamicObject object);

    long getEnd(DynamicObject object);

}
