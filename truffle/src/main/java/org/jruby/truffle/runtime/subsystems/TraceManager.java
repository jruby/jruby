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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.truffle.nodes.RubyGuards;
import com.oracle.truffle.api.object.DynamicObject;

public class TraceManager {

    private final CyclicAssumption traceAssumption = new CyclicAssumption("trace-func");
    private DynamicObject traceFunc = null;
    private boolean isInTraceFunc = false;

    public void setTraceFunc(DynamicObject traceFunc) {
        assert RubyGuards.isRubyProc(traceFunc);

        this.traceFunc = traceFunc;
        traceAssumption.invalidate();
    }

    public DynamicObject getTraceFunc() {
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
