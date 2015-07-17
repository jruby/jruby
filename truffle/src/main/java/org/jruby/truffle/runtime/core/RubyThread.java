/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.RubyThread.Status;
import org.jruby.truffle.nodes.core.ThreadNodes;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * Represents the Ruby {@code Thread} class. Implemented using Java threads, but note that there is
 * not a one-to-one mapping between Ruby threads and Java threads - specifically in combination with
 * fibers as they are currently implemented as their own Java threads.
 */
public class RubyThread extends RubyBasicObject {

    public static class ThreadFields {
        public final ThreadManager manager;

        public final FiberManager fiberManager;

        public String name;

        /**
         * We use this instead of {@link Thread#join()} since we don't always have a reference
         * to the {@link Thread} and we want to handle cases where the Thread did not start yet.
         */
        public final CountDownLatch finished = new CountDownLatch(1);

        public volatile Thread thread;
        public volatile Status status = Status.RUN;
        public volatile AtomicBoolean wakeUp = new AtomicBoolean(false);

        public volatile Object exception;
        public volatile Object value;

        public final RubyBasicObject threadLocals;

        public final List<Lock> ownedLocks = new ArrayList<>(); // Always accessed by the same underlying Java thread.

        public boolean abortOnException = false;

        public ThreadNodes.InterruptMode interruptMode = ThreadNodes.InterruptMode.IMMEDIATE;

        public ThreadFields(ThreadManager manager, FiberManager fiberManager, RubyBasicObject threadLocals) {
            this.manager = manager;
            this.fiberManager = fiberManager;
            this.threadLocals = threadLocals;
        }
    }

    public ThreadFields fields;

    public RubyThread(RubyClass rubyClass, ThreadManager manager) {
        super(rubyClass);
        fields = new ThreadFields(manager, new FiberManager(this, manager), new RubyBasicObject(rubyClass.getContext().getCoreLibrary().getObjectClass()));
    }

}
