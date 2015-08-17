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
import org.joda.time.DateTime;
import org.jruby.truffle.om.dsl.api.Layout;

@Layout
public interface TimeLayout extends BasicObjectLayout {

    DynamicObjectFactory createTimeShape(DynamicObject logicalClass, DynamicObject metaClass);

    DynamicObject createTime(DynamicObjectFactory factory, DateTime dateTime, Object offset);

    boolean isTime(DynamicObject object);

    DateTime getDateTime(DynamicObject object);

    void setDateTime(DynamicObject object, DateTime value);

    Object getOffset(DynamicObject object);

    void setOffset(DynamicObject object, Object value);

}
