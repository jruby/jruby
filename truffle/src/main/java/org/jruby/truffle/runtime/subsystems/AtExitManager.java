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

import org.jruby.truffle.runtime.core.RubyProc;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AtExitManager {

    private final Deque<RubyProc> runOnExit = new ConcurrentLinkedDeque<>();
    private final Deque<RubyProc> runOnExitAlways = new ConcurrentLinkedDeque<>();

    public void add(RubyProc block, boolean always) {
        if (always) {
            runOnExitAlways.push(block);
        } else {
            runOnExit.push(block);
        }
    }

    public void run(boolean normalExit) {
        try {
            if (normalExit) {
                runExitHooks(runOnExit);
            }
        } finally {
            runExitHooks(runOnExitAlways);
        }
    }

    private void runExitHooks(Deque<RubyProc> stack) {
        while (true) {
            RubyProc block;
            try {
                block = stack.pop();
            } catch (NoSuchElementException e) {
                break;
            }

            block.rootCall();
        }
    }

}
