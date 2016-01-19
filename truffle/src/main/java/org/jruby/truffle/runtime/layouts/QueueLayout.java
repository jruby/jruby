/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.object.ObjectType;

import org.jruby.truffle.om.dsl.api.Layout;

import java.util.concurrent.LinkedBlockingQueue;

@Layout
public interface QueueLayout extends BasicObjectLayout {

    DynamicObjectFactory createQueueShape(DynamicObject logicalClass,
                                          DynamicObject metaClass);

    DynamicObject createQueue(DynamicObjectFactory factory,
                              LinkedBlockingQueue<Object> queue);

    boolean isQueue(ObjectType objectType);
    boolean isQueue(DynamicObject object);

    LinkedBlockingQueue<Object> getQueue(DynamicObject object);

}
