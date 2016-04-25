/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.queue;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface QueueLayout extends BasicObjectLayout {

    DynamicObjectFactory createQueueShape(DynamicObject logicalClass,
                                          DynamicObject metaClass);

    DynamicObject createQueue(DynamicObjectFactory factory,
                              LinkedBlockingQueueLocksConditions<Object> queue);

    boolean isQueue(ObjectType objectType);
    boolean isQueue(DynamicObject object);

    LinkedBlockingQueueLocksConditions<Object> getQueue(DynamicObject object);

}
