/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyException;

/**
 * Rescues any exception.
 */
public class RescueAnyNode extends RescueNode {

    public RescueAnyNode(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection, body);
    }

    @Override
    public boolean canHandle(VirtualFrame frame, RubyException exception) {
        return true;
    }

}
