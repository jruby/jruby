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

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.nodes.core.ThreadNodes;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents the Ruby {@code Fiber} class. The current implementation uses Java threads and message
 * passing. Note that with fibers, a Ruby thread has multiple Java threads which interleave.
 * A {@code Fiber} is associated with a single (Ruby) {@code Thread}.
 */
@Deprecated
public class RubyFiber extends RubyBasicObject {

    public static class FiberFields {
        public final RubyBasicObject rubyThread;
        @CompilationFinal public String name;
        public final boolean isRootFiber;
        // we need 2 slots when the FiberManager sends the kill message and there is another message unprocessed
        public final BlockingQueue<FiberNodes.FiberMessage> messageQueue = new LinkedBlockingQueue<>(2);

        public LexicalScope lexicalScopeStack;

        public volatile RubyBasicObject lastResumedByFiber = null;
        public volatile boolean alive = true;
        public volatile Thread thread;

        public FiberFields(RubyBasicObject rubyThread, boolean isRootFiber) {
            assert RubyGuards.isRubyThread(rubyThread);
            this.rubyThread = rubyThread;
            this.isRootFiber = isRootFiber;
            this.lexicalScopeStack = rubyThread.getContext().getRootLexicalScope();
        }
    }

    public final FiberFields fields;

    public RubyFiber(RubyBasicObject parent, RubyBasicObject rubyClass, String name) {
        this(parent, ThreadNodes.getFiberManager(parent), ThreadNodes.getThreadManager(parent), rubyClass, name, false);
    }

    public RubyFiber(RubyBasicObject parent, FiberManager fiberManager, ThreadManager threadManager, RubyBasicObject rubyClass, String name, boolean isRootFiber) {
        super(rubyClass);
        fields = new FiberFields(parent, isRootFiber);
        FiberNodes.getFields(this).name = name;
    }

}
