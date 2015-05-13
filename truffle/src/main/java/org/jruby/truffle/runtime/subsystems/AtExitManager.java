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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class AtExitManager {

    private final List<RubyProc> runOnExit = new LinkedList<>();
    private final List<RubyProc> runOnExitAlways = new LinkedList<>();

    public synchronized void add(RubyProc block, boolean always) {
        if (always) {
            runOnExitAlways.add(block);
        } else {
            runOnExit.add(block);
        }
    }

    public synchronized void run(boolean normalExit) {
        if (normalExit) {
            Collections.reverse(runOnExit);

            for (RubyProc block : runOnExit) {
                try {
                    block.rootCall();
                } catch (Exception e) {
                }
            }
        }

        Collections.reverse(runOnExitAlways);

        for (RubyProc block : runOnExitAlways) {
            try {
                block.rootCall();
            } catch (Exception e) {
            }
        }
    }

}
