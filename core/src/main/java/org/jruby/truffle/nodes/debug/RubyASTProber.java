/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.instrument.ASTNodeProber;
import com.oracle.truffle.api.instrument.ASTProber;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.cli.Options;

import java.util.ArrayList;
import java.util.List;

public class RubyASTProber implements ASTProber {

    private final List<RubyNodeProber> probers = new ArrayList<>();

    public RubyASTProber() {
        if (RubyContext.TRACE) {
            probers.add(new TraceProber());
        }

        if (RubyContext.OBJECTSPACE) {
            probers.add(new ObjectSpaceSafepointProber());
        }
    }

    @Override
    public void addNodeProber(ASTNodeProber prober) throws IllegalArgumentException {
        if (prober instanceof RubyNodeProber) {
            probers.add((RubyNodeProber) prober);
        } else {
            throw new IllegalArgumentException("invalid prober for Ruby implementation");
        }
    }

    public RubyNode probeAsStatement(RubyNode node) {
        RubyNode result = node;

        for (RubyNodeProber nodeProber : probers) {
            result = nodeProber.probeAsStatement(result);
        }

        return result;
    }

    public RubyNode probeAsPeriodic(RubyNode node) {
        RubyNode result = node;

        for (RubyNodeProber nodeProber : probers) {
            result = nodeProber.probeAsPeriodic(result);
        }

        return result;
    }

    @Override
    public ASTNodeProber getCombinedNodeProber() {
        return null;
    }

}
