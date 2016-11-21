/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.time;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

import java.time.ZonedDateTime;

@Layout
public interface TimeLayout extends BasicObjectLayout {

    DynamicObjectFactory createTimeShape(DynamicObject logicalClass,
                                         DynamicObject metaClass);

    Object[] build(ZonedDateTime dateTime,
                    Object zone,
                    Object offset,
                    boolean relativeOffset,
                    boolean isUtc);

    boolean isTime(DynamicObject object);

    ZonedDateTime getDateTime(DynamicObject object);
    void setDateTime(DynamicObject object, ZonedDateTime value);

    Object getOffset(DynamicObject object);
    void setOffset(DynamicObject object, Object value);

    Object getZone(DynamicObject object);
    void setZone(DynamicObject object, Object value);

    boolean getRelativeOffset(DynamicObject object);
    void setRelativeOffset(DynamicObject object, boolean value);

    boolean getIsUtc(DynamicObject object);
    void setIsUtc(DynamicObject object, boolean value);

}
