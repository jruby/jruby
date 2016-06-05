/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.fiber;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import com.oracle.truffle.api.object.dsl.Volatile;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

@Layout
public interface FiberLayout extends BasicObjectLayout {

    DynamicObjectFactory createFiberShape(DynamicObject logicalClass,
                                          DynamicObject metaClass);

    DynamicObject createFiber(DynamicObjectFactory factory,
                              DynamicObject fiberLocals,
                              boolean rootFiber,
                              CountDownLatch initializedLatch,
                              BlockingQueue<FiberNodes.FiberMessage> messageQueue,
                              DynamicObject rubyThread,
                              @Volatile @Nullable DynamicObject lastResumedByFiber,
                              @Volatile boolean alive,
                              @Volatile @Nullable Thread thread);

    boolean isFiber(DynamicObject object);

    DynamicObject getFiberLocals(DynamicObject object);

    boolean getRootFiber(DynamicObject object);

    CountDownLatch getInitializedLatch(DynamicObject object);

    BlockingQueue<FiberNodes.FiberMessage> getMessageQueue(DynamicObject object);

    DynamicObject getRubyThread(DynamicObject object);

    DynamicObject getLastResumedByFiber(DynamicObject object);
    void setLastResumedByFiber(DynamicObject object, DynamicObject value);

    boolean getAlive(DynamicObject object);
    void setAlive(DynamicObject object, boolean value);

    Thread getThread(DynamicObject object);
    void setThread(DynamicObject object, Thread value);

}
