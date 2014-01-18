/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.runtime.*;

/**
 * Switches execution to the parallel {@link RubyNode#isDefined} semantic path. Represents the
 * {@code defined?} keyword in Ruby.
 */
@NodeInfo(shortName = "defined")
public class DefinedNode extends RubyNode {

    @Child protected RubyNode child;

    public DefinedNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = adoptChild(child);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return child.isDefined(frame);
    }

}
