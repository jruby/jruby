/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.nodes.instrument.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.debug.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.debug.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * Utility for instrumenting Ruby AST nodes to support the language's built-in <A
 * href="http://www.ruby-doc.org/core-2.0.0/Kernel.html#method-i-set_trace_func">tracing
 * facility</A>. It ignores nodes other than {@linkplain NodePhylum#STATEMENT statements}.
 */
final class DefaultRubyNodeInstrumenter implements RubyNodeInstrumenter {

    private final boolean trace;

    public DefaultRubyNodeInstrumenter(boolean trace) {
        this.trace = trace;
    }

    public RubyNode instrumentAsStatement(RubyNode rubyNode) {
        assert rubyNode != null;
        assert !(rubyNode instanceof RubyProxyNode);
        final RubyContext context = rubyNode.getContext();
        if (trace) {
            final RubyProxyNode proxy = new RubyProxyNode(context, rubyNode);
            proxy.markAs(NodePhylum.STATEMENT);
            proxy.getProbeChain().appendProbe(new RubyTraceProbe(context));
            return proxy;
        }
        return rubyNode;
    }

    public RubyNode instrumentAsCall(RubyNode node, String callName) {
        return node;
    }

    public RubyNode instrumentAsLocalAssignment(RubyNode node, UniqueMethodIdentifier methodIdentifier, String localName) {
        return node;
    }

}
