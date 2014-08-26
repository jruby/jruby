/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.signal;

import org.jruby.truffle.runtime.core.RubyProc;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class ProcSignalHandler implements SignalHandler {

    private final RubyProc proc;

    public ProcSignalHandler(RubyProc proc) {
        this.proc = proc;
    }

    @Override
    public void handle(Signal signal) {
        proc.rootCall();
    }

    public RubyProc getProc() {
        return proc;
    }
}
