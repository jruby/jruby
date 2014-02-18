/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.InlinableMethodImplementation;
import org.jruby.truffle.nodes.call.InlineHeuristic;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

public class InactiveTraceProbe extends RubyProbe {

    private final Assumption notTracingAssumption;

    public InactiveTraceProbe(RubyContext context, final Assumption notTracingAssumption) {
        super(context, false);
        this.notTracingAssumption = notTracingAssumption;
    }

    @Override
    public void enter(Node astNode, VirtualFrame frame) {
        try {
            notTracingAssumption.check();
        } catch (InvalidAssumptionException e) {
            final RubyProc traceProc = context.getTraceManager().getTraceProc();

            if (traceProc == null) {
                final InactiveTraceProbe newInactiveTraceProbe = new InactiveTraceProbe((RubyContext) getContext(), context.getTraceManager().getTracingAssumption().getAssumption());
                replace(newInactiveTraceProbe);
                newInactiveTraceProbe.enter(astNode, frame);
                return;
            }

            if (traceProc.getMethod().getImplementation() instanceof InlinableMethodImplementation && InlineHeuristic.shouldInlineTrace((InlinableMethodImplementation) traceProc.getMethod().getImplementation())) {
                final InlinableMethodImplementation inlinable = (InlinableMethodImplementation) traceProc.getMethod().getImplementation();
                final InlinedTraceProbe activeTraceProbe = new InlinedTraceProbe((RubyContext) getContext(), inlinable, context.getTraceManager().getTracingAssumption().getAssumption());
                replace(activeTraceProbe);
                activeTraceProbe.enter(astNode, frame);
            } else {
                throw new UnsupportedOperationException("We only support inlinable trace funcs at the moment");
            }

        }
    }
}
