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

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AtExitManager {

    private final RubyContext context;

    private final Deque<DynamicObject> runOnExit = new ConcurrentLinkedDeque<>();
    private final Deque<DynamicObject> runOnExitAlways = new ConcurrentLinkedDeque<>();

    public AtExitManager(RubyContext context) {
        this.context = context;
    }

    public void add(DynamicObject block, boolean always) {
        assert RubyGuards.isRubyProc(block);

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

    private void runExitHooks(Deque<DynamicObject> stack) {
        while (true) {
            DynamicObject block;
            try {
                block = stack.pop();
            } catch (NoSuchElementException e) {
                break;
            }

            try {
                ProcNodes.rootCall(block);
            } catch (RaiseException e) {
                final Object rubyException = e.getRubyException();

                for (String line : Backtrace.DISPLAY_FORMATTER.format(context, (DynamicObject) rubyException, Layouts.EXCEPTION.getBacktrace((DynamicObject) rubyException))) {
                    System.err.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
