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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Manages at_exit callbacks.
 */
public class AtExitManager {

    private final List<RubyProc> blocks = new LinkedList<>();

    public synchronized void add(RubyProc block) {
        blocks.add(block);
    }

    public synchronized void run() {
        final ListIterator<RubyProc> iterator = blocks.listIterator(blocks.size());

        while (iterator.hasPrevious()) {
            RubyProc hook = iterator.previous();
            iterator.remove();
            hook.rootCall();
        }
    }
}
