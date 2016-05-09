/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.tracepoint;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface TracePointLayout extends BasicObjectLayout {

    DynamicObjectFactory createTracePointShape(DynamicObject logicalClass,
                                               DynamicObject metaClass);

    DynamicObject createTracePoint(
            DynamicObjectFactory factory,
            @Nullable Class[] tags,
            @Nullable DynamicObject event,
            @Nullable DynamicObject path,
            int line,
            @Nullable DynamicObject binding,
            @Nullable DynamicObject proc,
            @Nullable EventBinding eventBinding,
            boolean insideProc);

    boolean isTracePoint(DynamicObject object);

    Class[] getTags(DynamicObject object);
    void setTags(DynamicObject object, Class[] value);

    DynamicObject getEvent(DynamicObject object);
    void setEvent(DynamicObject object, DynamicObject value);

    DynamicObject getPath(DynamicObject object);
    void setPath(DynamicObject object, DynamicObject value);

    int getLine(DynamicObject object);
    void setLine(DynamicObject object, int value);

    DynamicObject getBinding(DynamicObject object);
    void setBinding(DynamicObject object, DynamicObject value);

    DynamicObject getProc(DynamicObject object);
    void setProc(DynamicObject object, DynamicObject value);

    EventBinding getEventBinding(DynamicObject object);
    void setEventBinding(DynamicObject object, EventBinding value);

    boolean getInsideProc(DynamicObject object);
    void setInsideProc(DynamicObject object, boolean value);

}
