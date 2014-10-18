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

import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.runtime.RubyContext;

public class SafepointInstrument extends Instrument {

    private final RubyContext context;

    public SafepointInstrument(RubyContext context) {
        this.context = context;
    }

    @Override
    public void enter(Node node, VirtualFrame frame) {
        context.getSafepointManager().poll();
    }

}
