/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface IOLayout extends BasicObjectLayout {

    String I_BUFFER_IDENTIFIER = "@ibuffer";
    String LINE_NO_IDENTIFIER = "@lineno";
    String DESCRIPTOR_IDENTIFIER = "@descriptor";
    String MODE_IDENTIFIER = "@mode";

    DynamicObjectFactory createIOShape(DynamicObject logicalClass,
                                       DynamicObject metaClass);

    DynamicObject createIO(DynamicObjectFactory factory,
                           DynamicObject iBuffer,
                           int lineNo,
                           int descriptor,
                           int mode);

    boolean isIO(DynamicObject object);

    DynamicObject getIBuffer(DynamicObject object);

    int getLineNo(DynamicObject object);
    void setLineNo(DynamicObject object, int value);

    int getDescriptor(DynamicObject object);
    void setDescriptor(DynamicObject object, int value);

    int getMode(DynamicObject object);
    void setMode(DynamicObject object, int value);

}
