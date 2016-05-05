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
import org.joda.time.DateTime;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface TimeLayout extends BasicObjectLayout {

    DynamicObjectFactory createTimeShape(DynamicObject logicalClass,
                                         DynamicObject metaClass);

    DynamicObject createTime(DynamicObjectFactory factory,
                             DateTime dateTime,
                             long nSec,
                             Object zone,
                             Object offset,
                             boolean relativeOffset,
                             boolean isUtc);

    boolean isTime(DynamicObject object);

    DateTime getDateTime(DynamicObject object);
    void setDateTime(DynamicObject object, DateTime value);

    long getNSec(DynamicObject object);
    void setNSec(DynamicObject object, long value);

    Object getOffset(DynamicObject object);
    void setOffset(DynamicObject object, Object value);

    Object getZone(DynamicObject object);
    void setZone(DynamicObject object, Object value);

    boolean getRelativeOffset(DynamicObject object);
    void setRelativeOffset(DynamicObject object, boolean value);

    boolean getIsUtc(DynamicObject object);
    void setIsUtc(DynamicObject object, boolean value);

}
