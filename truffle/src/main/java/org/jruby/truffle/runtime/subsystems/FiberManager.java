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

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyFiber;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Ruby {@code Fiber} objects for a given Ruby thread.
 */
public class FiberManager {

    private final RubyBasicObject rootFiber;
    private RubyBasicObject currentFiber;
    private final Set<RubyBasicObject> runningFibers = Collections.newSetFromMap(new ConcurrentHashMap<RubyBasicObject, Boolean>());

    public FiberManager(RubyBasicObject rubyThread, ThreadManager threadManager) {
        this.rootFiber = FiberNodes.newRootFiber(rubyThread, this, threadManager);
        this.currentFiber = rootFiber;
    }

    public RubyBasicObject getRootFiber() {
        return rootFiber;
    }

    public RubyBasicObject getCurrentFiber() {
        return currentFiber;
    }

    public void setCurrentFiber(RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        currentFiber = fiber;
    }

    public void registerFiber(RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        runningFibers.add(fiber);
    }

    public void unregisterFiber(RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        runningFibers.remove(fiber);
    }

    public void shutdown() {
        for (RubyBasicObject fiber : runningFibers) {
            if (!((RubyFiber) fiber).fields.isRootFiber) {
                FiberNodes.shutdown(fiber);
            }
        }
    }

}
