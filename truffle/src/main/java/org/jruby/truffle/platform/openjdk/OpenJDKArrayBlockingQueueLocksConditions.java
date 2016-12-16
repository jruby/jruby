/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.openjdk;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jruby.truffle.core.queue.ArrayBlockingQueueLocksConditions;
import org.jruby.truffle.core.queue.DelegatingBlockingQueue;
import org.jruby.truffle.language.control.JavaException;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class OpenJDKArrayBlockingQueueLocksConditions<T>
        extends DelegatingBlockingQueue<T> implements ArrayBlockingQueueLocksConditions<T> {

    private static final MethodHandle LOCK_FIELD_GETTER
            = MethodHandleUtils.getPrivateGetter(ArrayBlockingQueue.class, "lock");

    private static final MethodHandle NOT_EMPTY_CONDITION_FIELD_GETTER
            = MethodHandleUtils.getPrivateGetter(ArrayBlockingQueue.class, "notEmpty");

    private static final MethodHandle NOT_FULL_CONDITION_FIELD_GETTER
            = MethodHandleUtils.getPrivateGetter(ArrayBlockingQueue.class, "notFull");

    private final ReentrantLock lock;
    private final Condition notEmptyCondition;
    private final Condition notFullCondition;

    @TruffleBoundary
    public OpenJDKArrayBlockingQueueLocksConditions(int capacity) {
        super(new ArrayBlockingQueue<>(capacity));

        final ArrayBlockingQueue<T> queue = (ArrayBlockingQueue<T>) getQueue();

        try {
            lock = (ReentrantLock) LOCK_FIELD_GETTER.invokeExact(queue);
            notEmptyCondition = (Condition) NOT_EMPTY_CONDITION_FIELD_GETTER.invokeExact(queue);
            notFullCondition = (Condition) NOT_FULL_CONDITION_FIELD_GETTER.invokeExact(queue);
        } catch (Throwable throwable) {
            throw new JavaException(throwable);
        }
    }

    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public Condition getNotEmptyCondition() {
        return notEmptyCondition;
    }

    @Override
    public Condition getNotFullCondition() {
        return notFullCondition;
    }

}
