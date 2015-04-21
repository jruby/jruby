/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyFiber;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Ruby {@code Fiber} objects.
 */
public class FiberManager {

    private final RubyFiber rootFiber;
    // FIXME (eregon): per ruby thread
    private RubyFiber currentFiber;

    private final Set<RubyFiber> runningFibers = Collections.newSetFromMap(new ConcurrentHashMap<RubyFiber, Boolean>());

    public FiberManager(RubyContext context) {
        rootFiber = new RubyFiber(context.getCoreLibrary().getFiberClass(), this, context.getThreadManager(), "root", true);
        currentFiber = rootFiber;
    }

    public RubyFiber getCurrentFiber() {
        return currentFiber;
    }

    public void setCurrentFiber(RubyFiber fiber) {
        currentFiber = fiber;
    }

    public void registerFiber(RubyFiber fiber) {
        runningFibers.add(fiber);
    }

    public void unregisterFiber(RubyFiber fiber) {
        runningFibers.remove(fiber);
    }

    public void shutdown() {
        for (RubyFiber fiber : runningFibers) {
            fiber.shutdown();
        }
    }

}
