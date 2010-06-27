/*
 * Copyright (C) 2009, 2010 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jruby.cext;

import java.util.concurrent.locks.ReentrantLock;
import org.jruby.runtime.ThreadContext;

final class ExecutionLock {

    private static final ReentrantLock lock = new ReentrantLock();

    private ExecutionLock() {
    }

    public static void acquire() {
        lock.lock();
    }

    public static void release(ThreadContext context) {
        try {
            if (lock.getHoldCount() == 1) {
                GC.cleanup(context);
            }
        } finally {
            lock.unlock();
        }
    }

    public static void releaseNoCleanup() {
        lock.unlock();
    }
}
