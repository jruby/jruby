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

import java.util.concurrent.locks.ReentrantLock;

@org.jruby.truffle.om.dsl.api.Layout
public interface MutexLayout extends BasicObjectLayout {

    DynamicObjectFactory createMutexShape(DynamicObject logicalClass,
                                          DynamicObject metaClass);

    DynamicObject createMutex(DynamicObjectFactory factory,
                              ReentrantLock lock);

    boolean isMutex(DynamicObject object);

    ReentrantLock getLock(DynamicObject object);

}
