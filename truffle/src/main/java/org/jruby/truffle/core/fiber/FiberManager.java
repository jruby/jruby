/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.fiber;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyGuards;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Ruby {@code Fiber} objects for a given Ruby thread.
 */
public class FiberManager {

    private final DynamicObject rootFiber;
    private DynamicObject currentFiber;
    private final Set<DynamicObject> runningFibers = Collections.newSetFromMap(new ConcurrentHashMap<DynamicObject, Boolean>());

    public FiberManager(RubyContext context, DynamicObject rubyThread) {
        this.rootFiber = FiberNodes.createRootFiber(context, rubyThread);
        this.currentFiber = rootFiber;
    }

    public DynamicObject getRootFiber() {
        return rootFiber;
    }

    public DynamicObject getCurrentFiber() {
        return currentFiber;
    }

    public void setCurrentFiber(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        currentFiber = fiber;
    }

    public void registerFiber(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        runningFibers.add(fiber);
    }

    public void unregisterFiber(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        runningFibers.remove(fiber);
    }

    @TruffleBoundary
    public void shutdown() {
        for (DynamicObject fiber : runningFibers) {
            if (!Layouts.FIBER.getRootFiber(fiber)) {
                FiberNodes.shutdown(fiber);
            }
        }
    }

}
