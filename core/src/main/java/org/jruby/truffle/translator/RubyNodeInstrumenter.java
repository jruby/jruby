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
import org.jruby.truffle.runtime.methods.*;

public interface RubyNodeInstrumenter {

    /**
     * Adds instrumentation support at a node that should be considered a member of
     * {@link NodePhylum#STATEMENT}, possibly returning a new {@link InstrumentationProxyNode} that
     * holds the original as its child.
     */
    RubyNode instrumentAsStatement(RubyNode node);

    /**
     * Adds instrumentation support at a node that should be considered a member of
     * {@link NodePhylum#CALL}, possibly returning a new {@link InstrumentationProxyNode} that holds
     * the original as its child.
     */
    RubyNode instrumentAsCall(RubyNode node, String callName);

    /**
     * Adds instrumentation support at a node that should be considered a member of
     * {@link NodePhylum#ASSIGNMENT}, possibly returning a new {@link InstrumentationProxyNode} that
     * holds the original as its child.
     */
    RubyNode instrumentAsLocalAssignment(RubyNode node, UniqueMethodIdentifier methodIdentifier, String localName);

}
