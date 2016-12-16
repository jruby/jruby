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

import org.jruby.truffle.core.queue.DelegatingBlockingQueue;
import org.jruby.truffle.core.queue.LinkedBlockingQueueLocksConditions;
import org.jruby.truffle.language.control.JavaException;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class OpenJDKLinkedBlockingQueueLocksConditions<T>
        extends DelegatingBlockingQueue<T> implements LinkedBlockingQueueLocksConditions<T> {

    private static final MethodHandle TAKE_LOCK_FIELD_GETTER
            = MethodHandleUtils.getPrivateGetter(LinkedBlockingQueue.class, "takeLock");

    private static final MethodHandle NOT_EMPTY_CONDITION_FIELD_GETTER
            = MethodHandleUtils.getPrivateGetter(LinkedBlockingQueue.class, "notEmpty");

    private final ReentrantLock lock;
    private final Condition notEmptyCondition;

    public OpenJDKLinkedBlockingQueueLocksConditions() {
        super(new LinkedBlockingQueue<>());

        final LinkedBlockingQueue<T> queue = (LinkedBlockingQueue<T>) getQueue();

        try {
            lock = (ReentrantLock) TAKE_LOCK_FIELD_GETTER.invokeExact(queue);
            notEmptyCondition = (Condition) NOT_EMPTY_CONDITION_FIELD_GETTER.invokeExact(queue);
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

}
