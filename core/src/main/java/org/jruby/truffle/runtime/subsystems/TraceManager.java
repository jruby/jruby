package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.utilities.AssumedValue;
import org.jruby.truffle.runtime.core.RubyProc;

/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
public class TraceManager {

    private final AssumedValue<RubyProc> traceFunc = new AssumedValue<>("trace-func", null);
    private boolean isInTraceFunc = false;

    public void setTraceFunc(RubyProc traceFunc) {
        this.traceFunc.set(traceFunc);
    }

    public RubyProc getTraceFunc() {
        return traceFunc.get();
    }

    public boolean isInTraceFunc() {
        return isInTraceFunc;
    }

    public void setInTraceFunc(boolean isInTraceFunc) {
        this.isInTraceFunc = isInTraceFunc;
    }

}
