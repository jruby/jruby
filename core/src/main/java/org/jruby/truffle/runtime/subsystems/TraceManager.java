package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.utilities.AssumedValue;
import com.oracle.truffle.api.utilities.CyclicAssumption;
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

    private CyclicAssumption traceAssumption = new CyclicAssumption("before-func");
    private RubyProc traceFunc = null;
    private boolean isInTraceFunc = false;

    public void setTraceFunc(RubyProc traceFunc) {
        this.traceFunc = traceFunc;
        traceAssumption.invalidate();
    }

    public RubyProc getTraceFunc() {
        return traceFunc;
    }

    public Assumption getTraceAssumption() {
        return traceAssumption.getAssumption();
    }

    public boolean isInTraceFunc() {
        return isInTraceFunc;
    }

    public void setInTraceFunc(boolean isInTraceFunc) {
        this.isInTraceFunc = isInTraceFunc;
    }

}
