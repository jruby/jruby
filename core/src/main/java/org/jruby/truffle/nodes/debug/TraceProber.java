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

import com.oracle.truffle.api.instrument.PhylumTag;
import com.oracle.truffle.api.instrument.StandardTag;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.RubyNode;

public class TraceProber implements RubyNodeProber {

    @Override
    public RubyNode probeAsStatement(RubyNode node) {
        final RubyWrapper wrapper;

        if (node instanceof RubyWrapper) {
            throw new UnsupportedOperationException();
        } else {
            wrapper = new RubyWrapper(node.getContext(), node.getEncapsulatingSourceSection(), node);
            wrapper.tagAs(StandardTag.STATEMENT);
        }

        wrapper.getProbe().addInstrument(new TraceInstrument(node.getContext(), node.getEncapsulatingSourceSection()));

        return wrapper;
    }

    @Override
    public Node probeAs(Node node, PhylumTag phylumTag, Object... objects) {
        throw new UnsupportedOperationException();
    }

}
