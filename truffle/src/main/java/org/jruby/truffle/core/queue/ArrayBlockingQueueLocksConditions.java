/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.queue;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBlockingQueueLocksConditions<T> extends DelegatingBlockingQueue<T> {

    private static final MethodHandle LOCK_FIELD_GETTER = MethodHandleUtils.getPrivateGetter(ArrayBlockingQueue.class, "lock");
    private static final MethodHandle NOT_EMPTY_CONDITION_FIELD_GETTER = MethodHandleUtils.getPrivateGetter(ArrayBlockingQueue.class, "notEmpty");
    private static final MethodHandle NOT_FULL_CONDITION_FIELD_GETTER = MethodHandleUtils.getPrivateGetter(ArrayBlockingQueue.class, "notFull");

    private final ReentrantLock lock;
    private final Condition notEmptyCondition;
    private final Condition notFullCondition;

    public ArrayBlockingQueueLocksConditions(int capacity) {
        super(new ArrayBlockingQueue<T>(capacity));

        final ArrayBlockingQueue<T> queue = (ArrayBlockingQueue<T>) getQueue();

        try {
            lock = (ReentrantLock) LOCK_FIELD_GETTER.invokeExact(queue);
            notEmptyCondition = (Condition) NOT_EMPTY_CONDITION_FIELD_GETTER.invokeExact(queue);
            notFullCondition = (Condition) NOT_FULL_CONDITION_FIELD_GETTER.invokeExact(queue);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public Condition getNotEmptyCondition() {
        return notEmptyCondition;
    }

    public Condition getNotFullCondition() {
        return notFullCondition;
    }

}
