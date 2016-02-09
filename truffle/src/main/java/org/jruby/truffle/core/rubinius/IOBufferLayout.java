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
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@org.jruby.truffle.om.dsl.api.Layout
public interface IOBufferLayout extends BasicObjectLayout {

    String WRITE_SYNCED_IDENTIFIER = "@write_synced";
    String STORAGE_IDENTIFIER = "@storage";
    String USED_IDENTIFIER = "@used";
    String TOTAL_IDENTIFIER = "@total";

    DynamicObjectFactory createIOBufferShape(DynamicObject logicalClass,
                                             DynamicObject metaClass);

    DynamicObject createIOBuffer(DynamicObjectFactory factory,
                                 boolean writeSynced,
                                 DynamicObject storage,
                                 int used,
                                 int total);

    boolean getWriteSynced(DynamicObject object);
    void setWriteSynced(DynamicObject object, boolean value);

    DynamicObject getStorage(DynamicObject object);
    void setStorage(DynamicObject object, DynamicObject value);

    int getUsed(DynamicObject object);
    void setUsed(DynamicObject object, int value);

    int getTotal(DynamicObject object);
    void setTotal(DynamicObject object, int value);

}
