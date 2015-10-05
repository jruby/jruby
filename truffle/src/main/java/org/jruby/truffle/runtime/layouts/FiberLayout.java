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
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.om.dsl.api.Volatile;

import java.util.concurrent.BlockingQueue;

@Layout
public interface FiberLayout extends BasicObjectLayout {

    DynamicObjectFactory createFiberShape(DynamicObject logicalClass,
                                          DynamicObject metaClass);

    DynamicObject createFiber(DynamicObjectFactory factory,
                              boolean rootFiber,
                              BlockingQueue<FiberNodes.FiberMessage> messageQueue,
                              DynamicObject rubyThread,
                              @Nullable String name,
                              @Volatile @Nullable DynamicObject lastResumedByFiber,
                              @Volatile boolean alive,
                              @Volatile @Nullable Thread thread);

    boolean isFiber(DynamicObject object);

    boolean getRootFiber(DynamicObject object);

    BlockingQueue<FiberNodes.FiberMessage> getMessageQueue(DynamicObject object);

    DynamicObject getRubyThread(DynamicObject object);

    String getName(DynamicObject object);
    void setName(DynamicObject object, String name);

    DynamicObject getLastResumedByFiber(DynamicObject object);
    void setLastResumedByFiber(DynamicObject object, DynamicObject lastResumedByFiber);

    boolean getAlive(DynamicObject object);
    void setAlive(DynamicObject object, boolean alive);

    Thread getThread(DynamicObject object);
    void setThread(DynamicObject object, Thread thread);

}
