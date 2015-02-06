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

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyThread;

import sun.misc.Signal;
import sun.misc.SignalHandler;

@SuppressWarnings("restriction")
public class ProcSignalHandler implements SignalHandler {

    private final RubyContext context;
    private final RubyProc proc;

    public ProcSignalHandler(RubyContext context, RubyProc proc) {
        this.context = context;
        this.proc = proc;
    }

    @Override
    public void handle(Signal signal) {
        RubyThread rubyThread = new RubyThread(context.getCoreLibrary().getThreadClass(), context.getThreadManager());

        rubyThread.run(context, null, Thread.currentThread().getName(), new Runnable() {
            @Override
            public void run() {
                proc.rootCall();
            }
        });
    }

}
