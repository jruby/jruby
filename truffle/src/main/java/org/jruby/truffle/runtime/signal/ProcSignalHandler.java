/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.signal;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.SafepointAction;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@SuppressWarnings("restriction")
public class ProcSignalHandler implements SignalHandler {

    private final RubyContext context;
    private final RubyBasicObject proc;

    public ProcSignalHandler(RubyContext context, RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);

        this.context = context;
        this.proc = proc;
    }

    @Override
    public void handle(Signal signal) {
        Thread mainThread = context.getThreadManager().getRootThread().getCurrentFiberJavaThread();
        context.getSafepointManager().pauseMainThreadAndExecuteLaterFromNonRubyThread(mainThread, new SafepointAction() {
            @Override
            public void run(RubyThread thread, Node currentNode) {
                ProcNodes.rootCall(proc);
            }
        });
    }

}
