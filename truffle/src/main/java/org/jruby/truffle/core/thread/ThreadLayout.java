/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.thread;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import com.oracle.truffle.api.object.dsl.Volatile;
import org.jruby.truffle.core.InterruptMode;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import org.jruby.truffle.core.fiber.FiberManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

@Layout
public interface ThreadLayout extends BasicObjectLayout {

    DynamicObjectFactory createThreadShape(
            DynamicObject logicalClass,
            DynamicObject metaClass);

    DynamicObject createThread(
            DynamicObjectFactory factory,
            DynamicObject threadLocals,
            @Volatile InterruptMode interruptMode, // needs to be volatile for fibers implemented by threads
            @Volatile ThreadStatus status,
            List<Lock> ownedLocks,
            @Nullable FiberManager fiberManager,
            CountDownLatch finishedLatch,
            boolean abortOnException,
            @Nullable @Volatile Thread thread,
            @Nullable @Volatile DynamicObject exception,
            @Nullable @Volatile Object value,
            AtomicBoolean wakeUp,
            @Volatile int priority,
            DynamicObject threadGroup,
            DynamicObject name);

    boolean isThread(ObjectType objectType);
    boolean isThread(DynamicObject object);

    FiberManager getFiberManager(DynamicObject object);
    void setFiberManagerUnsafe(DynamicObject object, FiberManager value);

    CountDownLatch getFinishedLatch(DynamicObject object);

    DynamicObject getThreadLocals(DynamicObject object);

    List<Lock> getOwnedLocks(DynamicObject object);

    boolean getAbortOnException(DynamicObject object);
    void setAbortOnException(DynamicObject object, boolean value);

    InterruptMode getInterruptMode(DynamicObject object);
    void setInterruptMode(DynamicObject object, InterruptMode value);

    Thread getThread(DynamicObject object);
    void setThread(DynamicObject object, Thread value);

    ThreadStatus getStatus(DynamicObject object);
    void setStatus(DynamicObject object, ThreadStatus value);

    DynamicObject getException(DynamicObject object);
    void setException(DynamicObject object, DynamicObject value);

    Object getValue(DynamicObject object);
    void setValue(DynamicObject object, Object value);

    AtomicBoolean getWakeUp(DynamicObject object);

    int getPriority(DynamicObject object);
    void setPriority(DynamicObject object, int value);

    DynamicObject getThreadGroup(DynamicObject object);
    void setThreadGroup(DynamicObject object, DynamicObject value);

    DynamicObject getName(DynamicObject object);
    void setName(DynamicObject object, DynamicObject value);

}
