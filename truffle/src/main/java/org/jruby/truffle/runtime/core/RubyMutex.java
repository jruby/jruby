/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Represents the Ruby {@code Mutex} class.
 */
public class RubyMutex extends RubyBasicObject {

    private final ReentrantLock lock = new ReentrantLock();

    public RubyMutex(RubyClass rubyClass) {
        super(rubyClass);
    }

    public ReentrantLock getReentrantLock() {
        return lock;
    }

    public static class MutexAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyMutex(rubyClass);
        }

    }

}
